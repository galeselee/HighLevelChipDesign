#include "diameter.h"
#include "utils.hpp"


void sssp_TB(unsigned int numVert,
             unsigned int numEdge,
             unsigned int source,
             unsigned int* offset,
             unsigned int* column,
             float* weight,
             float* distance) {
    for (int i = 0; i < numVert; i++) {
        distance[i] = std::numeric_limits<float>::infinity();
    }

    std::queue<unsigned int> q;

    q.push(source);
    distance[source] = 0;
    while (!q.empty()) {
        unsigned int tmp = q.front();
        for (int i = offset[tmp]; i < offset[tmp + 1]; i++) {
            float fromDist = distance[tmp];
            float toDist = distance[column[i]];
            float curDist = fromDist + weight[i];
            if (curDist < toDist) {
                distance[column[i]] = curDist;
                q.push(column[i]);
            }
        }
        q.pop();
    }
}

unsigned offset32[NUMVert + 10];
unsigned column32[NUMEdge + 10];
float weight32[NUMEdge + 10];

float max_dist[1];
unsigned source[1];
unsigned destination[1];

int main(int argc, const char* argv[]) {
    std::cout << "\n---------------------diameter Traversal Test----------------\n";


    int sourceID = 30;

    std::string offsetfile = "data/data-csr-offset.mtx";
    std::string columnfile = "data/data-csr-indicesweights.mtx";

    char line[1024] = {0};
    int index = 0;

    int numVert;
    int maxVertexId;
    int numEdges;

    std::fstream offsetfstream(offsetfile.c_str(), std::ios::in);
    if (!offsetfstream) {
        std::cout << "Error : " << offsetfile << " file doesn't exist !" << std::endl;
        exit(1);
    }

    offsetfstream.getline(line, sizeof(line));
    std::stringstream numOdata(line);
    numOdata >> numVert;
    numOdata >> maxVertexId;


    while (offsetfstream.getline(line, sizeof(line))) {
        std::stringstream data(line);
        data >> offset32[index];
        index++;
    }

    std::fstream columnfstream(columnfile.c_str(), std::ios::in);
    if (!columnfstream) {
        std::cout << "Error : " << columnfile << " file doesn't exist !" << std::endl;
        exit(1);
    }

    // read file finish
    std::cout << "File reading finish" << std::endl;
    index = 0;

    columnfstream.getline(line, sizeof(line));
    std::stringstream numCdata(line);
    numCdata >> numEdges;
    std::cout << "Number of edges is " << numEdges << std::endl;



    while (columnfstream.getline(line, sizeof(line))) {
        std::stringstream data(line);
        data >> column32[index];
        data >> weight32[index];
        index++;
    }

    std::cout << "Weight data reading finish" << std::endl;



    std::cout << "kernel start------" << std::endl;
    std::cout << "Input: numVertex=" << numVert << ", numEdges=" << numEdges << std::endl;
    assert(numVert == NUMVert);
    assert(numEdges == NUMEdge);
    diameter_kernel(offset32, column32, weight32, max_dist, source, destination);

    std::cout << "kernel end------" << std::endl;


    std::cout << "============================================================" << std::endl;
    unsigned errs = 0;

    float* distance = aligned_alloc<float>(numVert);
    sssp_TB(numVert, numEdges, source[0], offset32, column32, weight32, distance);
    if (std::fabs(distance[destination[0]] - max_dist[0]) / distance[destination[0]] > 0.0001) {
        std::cout << "source, destination, distance mismatch" << std::endl;
        errs++;
    }
    if (distance[destination[0]] == std::numeric_limits<float>::infinity()) {
        std::cout << "not linked source destination found" << std::endl;
        errs++;
    }
    std::cout << "source: " << source[0] << " destination: " << destination[0] << std::endl;
    std::cout << "435f8e47 calculated diameter: " << max_dist[0] << std::endl;

    return errs;
}
