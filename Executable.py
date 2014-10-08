"""
script to call a Workflow function as if it was standalone python script
"""

import Workflow
import argparse
import inspect
import sys

def main():
    """
    main 
    """

    met = [x[0] for x in inspect.getmembers(Workflow,
                                            predicate=inspect.isfunction)]
    parser = argparse.ArgumentParser()
    parser.add_argument("workflow", choices=met, help="the workflow name")
    parser.add_argument('a', nargs=argparse.REMAINDER,
                        help="specific parameters to pass to the workflow")
    args = parser.parse_args()

    if args.workflow in met:
        getattr(Workflow, args.workflow)(*args.a)
    
    return 0


if __name__ == "__main__":
    sys.exit(main())



